package nl.biopet.tools.validatefastq

import java.io.File

import nl.biopet.utils.tool.AbstractOptParser

class ArgsParser(cmdName: String) extends AbstractOptParser[Args](cmdName) {
  opt[File]("inputFile")
    .abbr("i")
    .unbounded()
    .required()
    .maxOccurs(1)
    .action((x, c) => c.copy(inputFile = x))
}
