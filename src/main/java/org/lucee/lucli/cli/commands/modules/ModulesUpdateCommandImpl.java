package org.lucee.lucli.cli.commands.modules;

import java.util.concurrent.Callable;

import org.lucee.lucli.modules.ModuleCommand;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * Direct implementation of modules update command
 */
@Command(
    name = "update",
    description = "Update a module from git"
)
public class ModulesUpdateCommandImpl implements Callable<Integer> {

    @Parameters(
        paramLabel = "MODULE_NAME",
        description = "Name of module to update"
    )
    private String moduleName;

    @Option(
        names = {"-u", "--url"},
        description = "Git URL to update from (e.g. https://github.com/user/repo.git[#ref])"
    )
    private String gitUrl;
    @Option(
        names = {"-r", "--ref", "--rev"},
        description = "Git ref to update to (branch, tag, or commit)"
    )
    private String ref;

    @Option(
        names = {"-f", "--force"},
        description = "Auto-approve module permissions (useful in non-interactive mode)"
    )
    private boolean force;

    @Override
    public Integer call() throws Exception {
        // Call updateModule directly - no arg parsing needed
        ModuleCommand.updateModule(moduleName, gitUrl, ref, force);
        return 0;
    }
}
