package nl.biopet.tools.validatefastq

import java.io.File

import nl.biopet.utils.tool.AbstractOptParser

class ArgsParser(cmdName: String) extends AbstractOptParser[Args](cmdName) {
  opt[File]('i', "fastq1") required () maxOccurs 1 valueName "<file>" action { (x, c) =>
    c.copy(input = x)
  }
  opt[File]('j', "fastq2") maxOccurs 1 valueName "<file>" action { (x, c) =>
    c.copy(input2 = Some(x))
  }
}
