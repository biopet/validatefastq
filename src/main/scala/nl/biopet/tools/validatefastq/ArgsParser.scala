package nl.biopet.tools.validatefastq

import java.io.File

import nl.biopet.utils.tool.{AbstractOptParser, ToolCommand}

class ArgsParser(toolCommand: ToolCommand[Args])
    extends AbstractOptParser[Args](toolCommand) {
  opt[File]('i', "fastq1") required () maxOccurs 1 valueName "<file>" action {
    (x, c) =>
      c.copy(input = x)
  } text "FASTQ file to be validated. (Required)"
  opt[File]('j', "fastq2") maxOccurs 1 valueName "<file>" action { (x, c) =>
    c.copy(input2 = Some(x))
  } text "Second FASTQ to be validated if FASTQs are paired. (Optional)"
}
