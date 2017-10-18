package nl.biopet.tools.validatefastq

import java.io.File

case class Args(input: File = null, input2: Option[File] = None)
