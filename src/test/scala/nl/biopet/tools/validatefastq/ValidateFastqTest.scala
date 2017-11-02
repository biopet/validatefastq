package nl.biopet.tools.validatefastq

import java.nio.file.Paths

import htsjdk.samtools.fastq.FastqRecord
import nl.biopet.utils.test.tools.ToolTest
import org.testng.annotations.{DataProvider, Test}

class ValidateFastqTest extends ToolTest[Args] {
  def toolCommand: ValidateFastq.type = ValidateFastq
  @Test
  def testNoArgs(): Unit = {
    intercept[IllegalArgumentException] {
      ValidateFastq.main(Array())
    }
  }

  @Test
  def testCheckMate(): Unit = {
    ValidateFastq.checkMate(new FastqRecord("read_1", "ATCG", "", "AAAA"),
      new FastqRecord("read_1", "ATCG", "", "AAAA"))

    intercept[IllegalStateException] {
      ValidateFastq.checkMate(new FastqRecord("read_1", "ATCG", "", "AAAA"),
        new FastqRecord("read_2", "ATCG", "", "AAAA"))
    }
  }

  @Test
  def testDuplicateCheck(): Unit = {
    ValidateFastq.duplicateCheck(new FastqRecord("read_1", "ATCG", "", "AAAA"), None)
    ValidateFastq.duplicateCheck(new FastqRecord("read_1", "ATCG", "", "AAAA"),
      Some(new FastqRecord("read_2", "ATCG", "", "AAAA")))

    intercept[IllegalStateException] {
      ValidateFastq.duplicateCheck(new FastqRecord("read_1", "ATCG", "", "AAAA"),
        Some(new FastqRecord("read_1", "ATCG", "", "AAAA")))
    }
  }

  @DataProvider(name = "providerGetPossibleEncodings")
  def providerGetPossibleEncodings = Array(
    Array(None, None, Nil),
    Array(Some('A'), None, Nil),
    Array(None, Some('A'), Nil),
    Array(Some('E'),
      Some('E'),
      List("Sanger", "Solexa", "Illumina 1.3+", "Illumina 1.5+", "Illumina 1.8+")),
    Array(Some('+'), Some('+'), List("Sanger", "Illumina 1.8+")),
    Array(Some('!'), Some('I'), List("Sanger", "Illumina 1.8+")),
    Array(Some('!'), Some('J'), List("Illumina 1.8+")),
    Array(Some(';'), Some('h'), List("Solexa")),
    Array(Some('@'), Some('h'), List("Solexa", "Illumina 1.3+")),
    Array(Some('C'), Some('h'), List("Solexa", "Illumina 1.3+", "Illumina 1.5+"))
  )

  @Test(dataProvider = "providerGetPossibleEncodings")
  def testGetPossibleEncodings(min: Option[Char], max: Option[Char], output: List[String]): Unit = {
    ValidateFastq.minQual = min
    ValidateFastq.maxQual = max
    ValidateFastq.getPossibleEncodings shouldBe output
  }

  @Test
  def testGetPossibleEncodingsFail(): Unit = {
    ValidateFastq.minQual = Some('!')
    ValidateFastq.maxQual = Some('h')
    ValidateFastq.getPossibleEncodings shouldBe Nil
  }

  @Test
  def testCheckQualEncoding(): Unit = {
    ValidateFastq.minQual = None
    ValidateFastq.maxQual = None
    ValidateFastq.checkQualEncoding(new FastqRecord("read_1", "ATCG", "", "AAAA"))
    ValidateFastq.getPossibleEncodings should not be Nil

    ValidateFastq.minQual = None
    ValidateFastq.maxQual = None

    ValidateFastq.checkQualEncoding(new FastqRecord("read_1", "ATCG", "", "A!hA"))
    ValidateFastq.getPossibleEncodings shouldBe Nil

    ValidateFastq.minQual = None
    ValidateFastq.maxQual = None

    ValidateFastq.checkQualEncoding(new FastqRecord("read_1", "ATCG", "", "hhhh"))
    ValidateFastq.checkQualEncoding(new FastqRecord("read_1", "ATCG", "", "!!!!"))
    ValidateFastq.getPossibleEncodings shouldBe Nil

    intercept[IllegalStateException] {
      ValidateFastq.minQual = None
      ValidateFastq.maxQual = None
      ValidateFastq.checkQualEncoding(new FastqRecord("read_1", "ATCG", "", "!! !!"))
    }
  }

  @Test
  def testValidFastqRecord(): Unit = {
    ValidateFastq.minQual = None
    ValidateFastq.maxQual = None
    ValidateFastq.validFastqRecord(new FastqRecord("read_1", "ATCG", "", "AAAA"))

    intercept[IllegalStateException] {
      ValidateFastq.validFastqRecord(new FastqRecord("read_1", "ATCG", "", "AAA"))
    }

    intercept[IllegalStateException] {
      ValidateFastq.validFastqRecord(new FastqRecord("read_1", "ATYG", "", "AAAA"))
    }
  }

  @Test
  def testMain(): Unit = {
    ValidateFastq.minQual = None
    ValidateFastq.maxQual = None
    val r1 = resourcePath("/paired01a.fq")
    val r2 = resourcePath("/paired01b.fq")
    ValidateFastq.main(Array("-i", r1, "-j", r2))

    //TODO: capture logs
    ValidateFastq.minQual = None
    ValidateFastq.maxQual = None
    val r2fail = resourcePath("/paired01c.fq")
    ValidateFastq.main(Array("-i", r1, "-j", r2fail))
  }

}
