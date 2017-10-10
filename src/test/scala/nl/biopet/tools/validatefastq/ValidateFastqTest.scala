package nl.biopet.tools.validatefastq

import nl.biopet.test.BiopetTest
import org.testng.annotations.Test

object ValidateFastqTest extends BiopetTest {
  @Test
  def testNoArgs(): Unit = {
    intercept[IllegalArgumentException] {
      ToolTemplate.main(Array())
    }
  }
}
