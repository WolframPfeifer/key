package org.key_project.proofmanagement

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import de.uka.ilkd.key.util.CommandLineException
import org.key_project.proofmanagement.check.CheckerData
import org.key_project.proofmanagement.check.ProofManagementException
import org.key_project.proofmanagement.io.LogLevel
import org.key_project.proofmanagement.merge.ProofBundleMerger
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*

class MainKt : CliktCommand() {

    override fun run() = Unit
}

class Check : CliktCommand() {
    val enableSettings by option(
            "--settings"
    ).flag()

    val enableDependency by option(
            "--dependency"
    ).flag()

    val enableMissing by option(
            "--missing"
    ).flag()

    val enableReplay by option(
            "--replay"
    ).flag()

    val enableReport by option(
            "--report"
    )

    override fun run() {
        val arguments: List<String> = commandLine.getArguments()
        if (arguments.size != 1) {
            commandLine.printUsage(System.out)
            return
        }

        var reportPath: Path? = null
        if (commandLine.isSet("--report")) {
            val outFileName: String = commandLine.getString("--report", "")
            reportPath = Paths.get(outFileName).toAbsolutePath()
        }

        val pathStr = arguments[0]
        val bundlePath = Paths.get(pathStr)
        Main.check(commandLine.isSet("--missing"), commandLine.isSet("--settings"),
                commandLine.isSet("--replay"), commandLine.isSet("--dependency"),
                bundlePath, reportPath)
    }
}

class Merge : CliktCommand() {
    override fun run() {
        val arguments = Main.CL_MERGE.arguments

        // at least three files!

        // at least three files!
        if (arguments.size < 3) {
            Main.CL_MERGE.printUsage(System.out)
            return
        }

        // at the moment only used for logging

        // at the moment only used for logging
        val logger = CheckerData(LogLevel.DEBUG)

        // convert Strings to Paths (for input and output)

        // convert Strings to Paths (for input and output)
        val inputs: MutableList<Path> = ArrayList()
        for (i in 0 until arguments.size - 1) {
            inputs.add(Paths.get(arguments[i]))
        }
        val output = Paths.get(arguments[arguments.size - 1])

        // Usually, the merging process is cancelled if there are conflicting files in both bundles.
        // This option forces merging. For the conflicting files, their versions from the first
        // bundle are taken.

        // Usually, the merging process is cancelled if there are conflicting files in both bundles.
        // This option forces merging. For the conflicting files, their versions from the first
        // bundle are taken.
        val force = Main.CL_MERGE.isSet("--force")

        try {
            ProofBundleMerger.merge(inputs, output, force, logger)
        } catch (e: ProofManagementException) {
            System.err.println("Error when trying to merge the bundles: ")
            e.printStackTrace()
            return
        }

        // perform a check on the newly created bundle with given commands

        // perform a check on the newly created bundle with given commands
        if (Main.CL_MERGE.isSet("--check")) {
            var checkParams = Main.CL_MERGE.getString("--check", "")

            // remove quotation marks
            checkParams = checkParams.substring(1, checkParams.length - 1)
            val temp = checkParams.trim { it <= ' ' }.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val newArgs = Arrays.copyOfRange(temp, 0, temp.size + 1)
            newArgs[newArgs.size - 1] = output.toString()
            try {
                Main.CL_MERGE_CHECK.parse(newArgs)
                Main.check(Main.CL_MERGE_CHECK)
            } catch (e: CommandLineException) {
                e.printStackTrace()
            }
        }
    }
}

fun main(args: Array<String>) = MainKt().subcommands(Check(), Merge()).main(args)
