
def buildHelper(String target)
{
	bat"""
		ubuntu run sh -c './build.sh ${target}' >> ${CIS_LOG} 2>&1
	"""
}


def executeBuild(String target, Map options)
{
    echo "-------------------------------------executeBuild ${target}-------------------------------------"
    
    //build project
	dir(options['projectName'])
    {
		deleteDir()
	}
	bat '''
			git clone https://github.com/amfdev/FFmpeg_dev.git FFmpeg
		'''
    dir(options['projectName'])
    {
		bat '''
			git clone https://github.com/amfdev/FFmpeg.git Sources
		'''
		
		dir("AMF/include/AMF")
		{
			
		}
		dir("scripts/AMF")
		{
			cis_checkout_scm("master", "https://github.com/GPUOpen-LibrariesAndSDKs/AMF.git")
			bat '''
				xcopy "/amf/public/include/*" "../AMF/include/AMF" /S /E
			'''
			deleteDir()
		}
		buildHelper(target)
		
    }

    dir("${options.projectName}_redist/${target}")
    {
        bat "echo ${target} > testout.txt"
        stash includes: '**/*', name: "app-${target}"
    }
	echo "-----------------------------------------end----------------------------------------------------"
}

def executeTests(String target, String profile, Map options)
{
	echo "-------------------------------------executeTests ${target}-------------------------------------"
    
    dir(target)
    {
        unstash "app-${target}"
        dir('bin')
        {
            bat "echo executeTests ${target}-${profile} >> ${CIS_LOG} 2>&1"
            bat "ffmpeg.exe -version >> ${CIS_LOG} 2>&1"
        }
    }
	echo "-----------------------------------------end----------------------------------------------------"
}

def executeDeploy(Map configMap, Map options)
{
    echo "-------------------------------------executeDeploy-------------------------------------"
    configMap.each()
    {
        dir(it.key)
        {
            unstash "app-${it.key}"
            dir('bin')
            {
                bat "echo deploy ${it.key} >> ${CIS_LOG} 2>&1"
                bat "ffmpeg.exe -version >> ${CIS_LOG} 2>&1"
            }
        }
    }
	echo "-----------------------------------------end----------------------------------------------------"
}

def call(Map userOptions = [:]
        ) {

    Map options = [
        config:'mingw:test',
        
        projectGroup:'AMF',

        projectName:'FFmpeg',
        projectBranch:'master',
        projectRepo:'https://github.com/amfdev/FFmpeg.git',

        projectName_AMF:'AMF',
        projectBranch_AMF:'master',
        projectRepo_AMF:'https://github.com/GPUOpen-LibrariesAndSDKs/AMF.git',
        
        'build.function':this.&executeBuild,
        'test.function':this.&executeTests,
        'deploy.function':this.&executeDeploy,

        'build.tag':'BuilderAMF',
        'build.platform.tag.mingw_gcc_x64':'mingw',
        'build.platform.tag.mingw_gcc_x86':'mingw',

        'test.tag':'TesterAMF',
        'test.cleandir':true,
        'test.platform.tag.mingw_gcc_x64':'Windows',
        'test.platform.tag.mingw_gcc_x86':'Windows',
        
        'deploy.tag':'DeployerAMF',
        'deploy.cleandir':true
    ]
    
    userOptions.each()
    {
        options[it.key]=it.value
    }
    
    cis_multiplatform_pipeline(options)
}
