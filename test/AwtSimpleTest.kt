import org.junit.Test
import javax.swing.JFrame

class AwtSimpleTest {
	@Test
	fun `test create from frame to check headless on travis`() {
		val frame = JFrame("hello world!")
		//frame.isVisible = true
		frame.dispose()
	}
}