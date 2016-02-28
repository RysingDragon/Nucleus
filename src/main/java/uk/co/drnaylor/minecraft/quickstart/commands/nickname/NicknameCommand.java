/*
 * This file is part of QuickStart, licensed under the MIT License (MIT). See the LICENCE.txt file
 * at the root of this project for more details.
 */
package uk.co.drnaylor.minecraft.quickstart.commands.nickname;

import com.google.inject.Inject;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import uk.co.drnaylor.minecraft.quickstart.Util;
import uk.co.drnaylor.minecraft.quickstart.api.PluginModule;
import uk.co.drnaylor.minecraft.quickstart.argumentparsers.UserParser;
import uk.co.drnaylor.minecraft.quickstart.config.MainConfig;
import uk.co.drnaylor.minecraft.quickstart.internal.CommandBase;
import uk.co.drnaylor.minecraft.quickstart.internal.annotations.Modules;
import uk.co.drnaylor.minecraft.quickstart.internal.annotations.Permissions;
import uk.co.drnaylor.minecraft.quickstart.internal.annotations.RegisterCommand;
import uk.co.drnaylor.minecraft.quickstart.internal.interfaces.InternalQuickStartUser;
import uk.co.drnaylor.minecraft.quickstart.internal.permissions.PermissionInformation;
import uk.co.drnaylor.minecraft.quickstart.internal.permissions.SuggestedLevel;
import uk.co.drnaylor.minecraft.quickstart.internal.services.UserConfigLoader;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

@RegisterCommand({ "nick", "nickname" })
@Permissions
@Modules(PluginModule.NICKNAME)
public class NicknameCommand extends CommandBase<CommandSource> {

    @Inject private UserConfigLoader loader;
    @Inject private MainConfig mainConfig;

    private final String playerKey = "player";
    private final String nickName = "nickname";

    private final Pattern colourPattern = Pattern.compile("&[0-9a-f]", Pattern.CASE_INSENSITIVE);
    private final Pattern stylePattern = Pattern.compile("&[omnl]", Pattern.CASE_INSENSITIVE);
    private final Pattern magicPattern = Pattern.compile("&k", Pattern.CASE_INSENSITIVE);

    @Override
    public Map<String, PermissionInformation> permissionSuffixesToRegister() {
        Map<String, PermissionInformation> m = new HashMap<>();
        m.put("others", new PermissionInformation(Util.getMessageWithFormat("permission.nick.others"), SuggestedLevel.ADMIN));
        m.put("colour", new PermissionInformation(Util.getMessageWithFormat("permission.nick.colour"), SuggestedLevel.ADMIN));
        m.put("color", new PermissionInformation(Util.getMessageWithFormat("permission.nick.colour"), SuggestedLevel.ADMIN));
        m.put("style", new PermissionInformation(Util.getMessageWithFormat("permission.nick.style"), SuggestedLevel.ADMIN));
        m.put("magic", new PermissionInformation(Util.getMessageWithFormat("permission.nick.magic"), SuggestedLevel.ADMIN));
        return m;
    }

    @Override
    public CommandSpec createSpec() {
        return CommandSpec.builder().arguments(
                GenericArguments.optionalWeak(GenericArguments.requiringPermission(GenericArguments.onlyOne(new UserParser(Text.of(playerKey))), permissions.getPermissionWithSuffix("others"))),
                GenericArguments.onlyOne(GenericArguments.string(Text.of(nickName)))
        ).executor(this).build();
    }

    @Override
    public CommandResult executeCommand(CommandSource src, CommandContext args) throws Exception {
        Optional<User> opl = this.getUser(User.class, src, playerKey, args);
        if (opl.isPresent()) {
            return CommandResult.empty();
        }

        User pl = opl.get();
        String name = args.<String>getOne(nickName).get();

        // Giving player must have the colour permissions and whatnot. Also, colour and color are the two spellings we support. (RULE BRITANNIA!)
        if (colourPattern.matcher(name).find() && (permissions.testSuffix(src, "colour") || permissions.testSuffix(src, "color"))) {
            src.sendMessage(Text.of(TextColors.RED, Util.getMessageWithFormat("command.nick.colour.noperms")));
            return CommandResult.empty();
        }

        // Giving player must have the colour permissions and whatnot.
        if (magicPattern.matcher(name).find() && permissions.testSuffix(src, "magic")) {
            src.sendMessage(Text.of(TextColors.RED, Util.getMessageWithFormat("command.nick.magic.noperms")));
            return CommandResult.empty();
        }

        // Giving player must have the colour permissions and whatnot.
        if (stylePattern.matcher(name).find() && permissions.testSuffix(src, "style")) {
            src.sendMessage(Text.of(TextColors.RED, Util.getMessageWithFormat("command.nick.style.noperms")));
            return CommandResult.empty();
        }

        // Do a regex remove to check minimum length requirements.
        if (name.replaceAll("&[0-9a-fomlnk]","").length() < Math.max(mainConfig.getMinNickLength(), 1)) {
            src.sendMessage(Text.of(TextColors.RED, Util.getMessageWithFormat("command.nick.tooshort")));
            return CommandResult.empty();
        }

        InternalQuickStartUser internalQuickStartUser = loader.getUser(pl);
        internalQuickStartUser.setNickname(name);
        Text set = internalQuickStartUser.getNicknameAsText().get();

        if (!src.equals(pl)) {
            src.sendMessage(Text.of(TextColors.GREEN, Util.getMessageWithFormat("command.nick.success.other", pl.getName()) + " - ",
                    TextColors.RESET, set));
        }

        if (pl.isOnline()) {
            pl.getPlayer().get().sendMessage(Text.of(TextColors.GREEN, Util.getMessageWithFormat("command.nick.success") + " - ",
                    TextColors.RESET, set));
        }

        return CommandResult.success();
    }
}