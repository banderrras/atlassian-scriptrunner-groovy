package com.banderrras

import com.onresolve.scriptrunner.runner.ScriptRunner
import com.onresolve.scriptrunner.runner.ScriptRunnerImpl
import java.nio.file.Files
import java.io.File
import groovy.util.logging.Log4j

@Singleton
@Log4j
public class SRProperties {

    final String configFileName = "srproperties.properties"

    public Properties getProperties() {
        def scriptRoots = ScriptRunnerImpl.getPluginComponent(ScriptRunner).getRootsForDisplay()?.split(", ")?.toList()
        
        File propertiesFile = null
        for (root in scriptRoots) { 
            propertiesFile = new File("$root/$configFileName")
            if (Files.isReadable(propertiesFile.toPath())) {
                log.info "Found ${propertiesFile.toPath()}"
                break   
            }
        }
        if(propertiesFile != null)
        {
            Properties properties = new Properties()
            propertiesFile.withInputStream {
                properties.load(it)
            }
            return properties
        }    
        else
            return null
    }
}