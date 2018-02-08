/*
 * Copyright (c) 2014 Biopet
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package nl.biopet.tools.validatefastq

import htsjdk.samtools.fastq.{FastqReader, FastqRecord}
import nl.biopet.utils.tool.ToolCommand

import scala.collection.mutable.ListBuffer
import scala.util.matching.Regex
import scala.collection.JavaConversions._

object ValidateFastq extends ToolCommand[Args] {
  def emptyArgs: Args = Args()
  def argsParser = new ArgsParser(this)
  def main(args: Array[String]): Unit = {
    val cmdArgs = cmdArrayToArgs(args)

    logger.info("Start")

    //read in fastq file 1 and if present fastq file 2
    val readFq1 = new FastqReader(cmdArgs.input)
    val readFq2 = cmdArgs.input2.map(new FastqReader(_))

    //define a counter to track the number of objects passing through the for loop

    var counter = 0

    try {
      //Iterate over the fastq file check for the length of both files if not correct, exit the tool and print error message

      var lastRecordR1: Option[FastqRecord] = None
      var lastRecordR2: Option[FastqRecord] = None
      for (recordR1 <- readFq1.iterator()) {
        counter += 1
        if (!readFq2.forall(_.hasNext))
          throw new IllegalStateException("R2 contains less reads then R1")

        //Getting R2 record, None if it's single end
        val recordR2 = readFq2.map(_.next())

        validFastqRecord(recordR1)
        duplicateCheck(recordR1, lastRecordR1)

        //Here we check if the readnames of both files are concordant, and if the sequence content are correct DNA/RNA sequences
        recordR2 match {
          case Some(r2) => // Paired End
            validFastqRecord(r2)
            duplicateCheck(r2, lastRecordR2)
            checkMate(recordR1, r2)
          case _ => // Single end
        }
        if (counter % 1e5 == 0)
          logger.info(
            counter + (if (recordR2.isDefined) " pairs"
                       else " reads") + " processed")
        lastRecordR1 = Some(recordR1)
        lastRecordR2 = recordR2
      }

      //if R2 is longer then R1 print an error code and exit the tool
      if (readFq2.exists(_.hasNext))
        throw new IllegalStateException("R2 contains more reads then R1")

      getPossibleEncodings match {
        case l if l.nonEmpty =>
          logger.info(s"Possible quality encodings found: ${l.mkString(", ")}")
        case _ => logger.warn(s"No possible quality encodings found")
      }

      logger.info(s"Done processing $counter fastq records, no errors found")
    } catch {
      case e: IllegalStateException =>
        logger.error(
          s"Error found at readnumber: $counter, linenumber ${(counter * 4) - 3}")
        logger.error(e.getMessage)
    }

    //close both iterators
    readFq1.close()
    readFq2.foreach(_.close())

    logger.info("Done")
  }

  private[tools] var minQual: Option[Char] = None
  private[tools] var maxQual: Option[Char] = None

  /**
    * This method checks if the encoding in a fastq record is correct
    * @param record The fastq record to check
    * @throws IllegalStateException Throws this when an error is ofund during checking
    */
  private[tools] def checkQualEncoding(record: FastqRecord): Unit = {
    val min = record.getBaseQualityString.min
    val max = record.getBaseQualityString.max
    if (!minQual.exists(_ <= min)) {
      minQual = Some(min)
      getPossibleEncodings
    }
    if (!maxQual.exists(_ >= max)) {
      maxQual = Some(max)
      getPossibleEncodings
    }
  }

  /**
    * This method returns the possible encodings till now
    * @return List of possible encodings
    * @throws IllegalStateException Throws this when an error is ofund during checking
    */
  private[tools] def getPossibleEncodings: List[String] = {
    val buffer: ListBuffer[String] = ListBuffer()
    (minQual, maxQual) match {
      case (Some(min), Some(max)) =>
        if (min < '!' || max > '~')
          throw new IllegalStateException(
            s"Quality is out of ascii range 33-126.  minQual: '$min', maxQual: '$max'")
        if (min >= '!' && max <= 'I') buffer += "Sanger"
        if (min >= ';' && max <= 'h') buffer += "Solexa"
        if (min >= '@' && max <= 'h') buffer += "Illumina 1.3+"
        if (min >= 'C' && max <= 'h') buffer += "Illumina 1.5+"
        if (min >= '!' && max <= 'J') buffer += "Illumina 1.8+"
      case _ =>
    }
    buffer.toList
  }

  val allowedBases: Regex = """([actgnACTGN+]+)""".r

  /**
    * This function checks for duplicates.
    * @param current currect fastq record
    * @param before fastq record before the current record
    * @throws IllegalStateException Throws this when an error is ofund during checking
    */
  def duplicateCheck(current: FastqRecord, before: Option[FastqRecord]): Unit = {
    if (before.exists(_.getReadHeader == current.getReadHeader))
      throw new IllegalStateException("Duplicate read ID found")
  }

  /**
    * This method will check if fastq record is correct
    * @param record Fastq record to check
    * @throws IllegalStateException Throws this when an error is ofund during checking
    */
  def validFastqRecord(record: FastqRecord): Unit = {
    checkQualEncoding(record)
    record.getReadString match {
      case allowedBases(_) =>
      case _ =>
        throw new IllegalStateException(s"Non IUPAC symbols identified")
    }
    if (record.getReadString.length != record.getBaseQualityString.length)
      throw new IllegalStateException(
        s"Sequence length does not match quality length")
  }

  /**
    * This method checks if the pair is the same ID
    * @param r1 R1 fastq record
    * @param r2 R2 fastq record
    * @throws IllegalStateException Throws this when an error is ofund during checking
    */
  def checkMate(r1: FastqRecord, r2: FastqRecord): Unit = {
    val id1 = r1.getReadHeader.takeWhile(_ != ' ')
    val id2 = r2.getReadHeader.takeWhile(_ != ' ')
    if (!(id1 == id2 ||
          id1.stripSuffix("/1") == id2.stripSuffix("/2") ||
          id1.stripSuffix(".1") == id2.stripSuffix(".2")))
      throw new IllegalStateException(
        s"Sequence headers do not match. R1: '${r1.getReadHeader}', R2: '${r2.getReadHeader}'")
  }

  def descriptionText: String =
    s"""
      |This tool validates a FASTQ file. When data is paired it can
      |also validate a pair of FASTQ files.
      |$toolName will check if the FASTQ is in valid FASTQ format.
      |This includes checking for duplicate reads and checking whether
      |a pair of FASTQ files contains the same amount of reads and headers match.
      |It also check whether the quality encodings are correct and outputs
      |the most likely encoding format (Sanger, Solexa etc.).
    """.stripMargin

  def manualText: String =
    s"""
       |$toolName validates the following things:
       |
       |- If paired: whether both fastqs have the same amount of reads
       |- If paired: whether sequence headers match.
       |- Whether the quality encoding is of the same length as the sequence in a read
       |- Whether the sequence consists of AGTC only. Regex: `$allowedBases`
       |- Whether the quality encoding is within a valid ASCII range
     """.stripMargin

  def exampleText: String =
    s"""
       | To validate a fastq file use:
       | ${example("-i", "input.fastq")}
       |
       | To validate a pair of fastq files use:
       | ${example("-i", "input.fastq", "-j", "input2.fastq")}
     """.stripMargin
}
