package nl.biopet.tools.validatefastq

import nl.biopet.test.BiopetTest
import org.testng.annotations.Test

class ValidateFastqTest extends BiopetTest {
  @Test
  def testNoArgs(): Unit = {
    intercept[IllegalArgumentException] {
      ValidateFastq.main(Array())
    }
  }
}
