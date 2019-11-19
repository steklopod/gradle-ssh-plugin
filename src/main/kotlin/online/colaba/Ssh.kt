package online.colaba

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.*
import org.hidetake.groovy.ssh.core.Remote
import org.hidetake.groovy.ssh.core.RunHandler
import org.hidetake.groovy.ssh.core.Service
import org.hidetake.groovy.ssh.session.SessionHandler


class Ssh : Plugin<Project> {

    override fun apply(project: Project): Unit = project.run {
        description = "FTP deploy needed ssh-tasks"

        registerPublisherTask()

        ssh {
            command = "echo \$PWD"
        }

        tasks {
            val publishFront by registering(SshCopy::class) {
                directory = SshCopy.frontendBuildFolder; group = sshGroup
            }
            val publishBack by registering(SshCopy::class) {
                directory = SshCopy.backendDistFolder; group = sshGroup
            }
            val publish by registering{
                dependsOn(publishFront); finalizedBy(publishBack)
            }
        }
    }
}

fun Service.runSessions(action: RunHandler.() -> Unit) = run(delegateClosureOf(action))
fun RunHandler.session(vararg remotes: Remote, action: SessionHandler.() -> Unit) = session(*remotes, delegateClosureOf(action))
fun SessionHandler.put(from: Any, into: Any) = put(hashMapOf("from" to from, "into" to into))
