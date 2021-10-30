package ModularsMusic;

import com.sedmelluq.discord.lavaplayer.player.*;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.*;
import com.sedmelluq.discord.lavaplayer.track.playback.NonAllocatingAudioFrameBuffer;
import discord4j.core.DiscordClient;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.VoiceState;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.channel.VoiceChannel;
import discord4j.voice.AudioProvider;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModularsMusic {

    public static void main(String[] args){

        //Audio Player Creation & Optimization
        final AudioPlayerManager playerManager = new DefaultAudioPlayerManager();
        playerManager.getConfiguration()
                .setFrameBufferFactory(NonAllocatingAudioFrameBuffer::new);

        //Allows parsing of remote sources i.e. YT Link
        AudioSourceManagers.registerRemoteSources(playerManager);

        // Create an AudioPlayer so Discord4J can receive audio data
        final AudioPlayer player = playerManager.createPlayer();

        AudioProvider provider = new LavaPlayerAudioProvider(player);


        //Creating Map for Commands and Commands List
        final Map<String, Command> commands = new HashMap<>();
        commands.put("ping", event -> event.getMessage()
                    .getChannel().block()
                    .createMessage("Pong!").block());

        commands.put("join", event -> {
            final Member member =event.getMember().orElse(null);
            if (member != null) {
                final VoiceState voiceState = member.getVoiceState().block();
                if (voiceState != null) {
                    final VoiceChannel channel =voiceState.getChannel().block();
                    if (channel != null) {
                        channel.join(spec-> spec.setProvider(provider)).block();
                    }
                }
            }        });
        final TrackScheduler scheduler = new TrackScheduler(player);
        commands.put("play", event -> {
            final String content = event.getMessage().getContent();
            final List<String> command = Arrays.asList(content.split(" "));
            playerManager.loadItem(command.get(1), scheduler);
        });

        commands.put("search", event -> {
            final String content = event.getMessage().getContent();
            String query = "ytsearch:" + content.replace("!search", "");
            final AudioPlaylist queryPlaylist = new BasicAudioPlaylist(query,null,null,true) {};
            final AudioTrack queryTrack = queryPlaylist.getSelectedTrack();
            //final AudioReference queryReference = queryTrack.getIdentifier();
            playerManager.loadItem(queryTrack.getIdentifier(),player);
            });


                    /*FunctionalResultHandler(playlist -> {
                player.playTrack(playlist.getTracks().get(0));
            }, null, null)); */




        /* Add A Command
        commands.put("Message", event -> event.getMessage()
                    .getChannel().block()
                    .createMessage("Response").block()); */


        final discord4j.core.GatewayDiscordClient client = DiscordClientBuilder.create(args[0]).build()
                .login()
                .block();
    client.getEventDispatcher().on(MessageCreateEvent.class)
    // subscribe is like block, in that it will *request* for action
    // to be done, but instead of blocking the thread, waiting for it
    // to finish, it will just execute the results asynchronously.
        .subscribe(event -> {
        // 3.1 Message.getContent() is a String
        final String content = event.getMessage().getContent();

        for (final Map.Entry<String, Command> entry : commands.entrySet()) {
            // We will be using ! as our "prefix" to any command in the system.
            if (content.startsWith('!' + entry.getKey())) {
                entry.getValue().execute(event);
                break;
            }
        }
    });
        client.onDisconnect().block();
    }
}

interface Command {
    void execute(MessageCreateEvent event);
}