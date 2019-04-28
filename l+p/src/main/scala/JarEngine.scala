import io.gatling.app.Gatling
import io.gatling.core.config.GatlingPropertiesBuilder

object JarEngine extends App {

	val props = new GatlingPropertiesBuilder()
		.resourcesDirectory("jar")
		.binariesDirectory("jar")
		.resultsDirectory(System.getProperty("resultsFolder"))
		.simulationClass(System.getProperty("simulationClass"))
	Gatling.fromMap(props.build)
	sys.exit()
}
