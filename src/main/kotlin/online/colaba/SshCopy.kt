package online.colaba

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.hidetake.groovy.ssh.Ssh
import org.hidetake.groovy.ssh.core.Remote
import java.io.File

const val sshGroup = "ssh"

open class SshCopy : DefaultTask() {
    init {
        group = sshGroup
        description = "Publish by FTP your distribution with SSH commands"
    }

    companion object {
        private const val defaultUser = "root"
        private const val defaultHost = "colaba.online"

        private val defaultRsaPath = "$userHomePath/.ssh/id_rsa".normalizeForWindows()

        private const val backendBuildFolder = "$backendService/$buildGroup"
        const val backendDistFolder = "$backendBuildFolder/libs"
        const val frontendBuildFolder = "$frontendService/dist"

        private fun connect(
                targetHost: String? = defaultHost, sshUser: String? = defaultUser, idRsaPath: String? = defaultRsaPath
        ) = Remote(sshUser).apply { host = targetHost; user = sshUser; identity = File(idRsaPath) }
    }

    @get:Input
    var host = defaultHost
    @get:Input
    var user: String = defaultUser
    @get:Input
    var idRsaPath: String = defaultRsaPath
    @get:Input
    @Optional
    var directory: String? = null
    @get:Input
    @Optional
    var toFolder: String? = null
    @get:Input
    @Optional
    var command: String? = null

    private val server = connect(targetHost = host, sshUser = user, idRsaPath = idRsaPath)
    private val ssh = Ssh.newService()

    @TaskAction
    fun run() {
        val rootDir = project.rootDir
        val fromLocalPath = "$rootDir/$directory".normalizeForWindows()
        val toRemoteDefault = fromLocalPath.substringAfter(rootDir.name + "/").substringBeforeLast("/")
        val toRemote = "${rootDir.name}/${toFolder ?: toRemoteDefault}"

        ssh.runSessions {
            session(server) {

                command?.let {
                    println("\uD83D\uDD11 Executing  commands on remote server ($host): [ $command ]")
                    execute(it)
                }

                directory?.let {
                    execute("rm -fr $toRemote")
                    execute("mkdir --parent $toRemote")

                    println("\uD83D\uDCE6 Copying from local: $fromLocalPath")
                    println("\uD83D\uDD25 Moving to remote  : $toRemote")
                    put(File(fromLocalPath), toRemote)
                }
            }
        }
    }

}

fun Project.registerPublisherTask() = tasks.register<SshCopy>(sshGroup)

val Project.ssh: TaskProvider<SshCopy>
    get() = tasks.named<SshCopy>(sshGroup)
