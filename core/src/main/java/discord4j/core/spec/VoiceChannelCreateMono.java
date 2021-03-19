/*
 * This file is part of Discord4J.
 *
 * Discord4J is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Discord4J is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Discord4J.  If not, see <http://www.gnu.org/licenses/>.
 */
package discord4j.core.spec;

import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.PermissionOverwrite;
import discord4j.core.object.entity.channel.Category;
import discord4j.core.object.entity.channel.Channel;
import discord4j.core.object.entity.channel.VoiceChannel;
import discord4j.core.util.EntityUtil;
import discord4j.discordjson.json.ImmutableChannelCreateRequest;
import discord4j.discordjson.json.OverwriteData;
import discord4j.discordjson.possible.Possible;
import discord4j.rest.util.Permission;
import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;

import java.util.Set;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

/**
 * A mono used to configure and create a {@link VoiceChannel}.
 */
public class VoiceChannelCreateMono extends AuditableRequest<VoiceChannel, ImmutableChannelCreateRequest.Builder, VoiceChannelCreateMono> {

    private final GatewayDiscordClient gateway;
    private final long guildId;

    public VoiceChannelCreateMono(Supplier<ImmutableChannelCreateRequest.Builder> requestBuilder, @Nullable String reason,
                                  GatewayDiscordClient gateway, long guildId) {
        super(requestBuilder, reason);
        this.gateway = gateway;
        this.guildId = guildId;
    }

    public VoiceChannelCreateMono(GatewayDiscordClient gateway, long guildId) {
        this(() -> ImmutableChannelCreateRequest.builder().type(Channel.Type.GUILD_VOICE.getValue()), null, gateway, guildId);
    }

    @Override
    public VoiceChannelCreateMono withReason(String reason) {
        return new VoiceChannelCreateMono(requestBuilder, reason, gateway, guildId);
    }

    @Override
    VoiceChannelCreateMono withBuilder(UnaryOperator<ImmutableChannelCreateRequest.Builder> f) {
        return new VoiceChannelCreateMono(apply(f), reason, gateway, guildId);
    }

    /**
     * Sets the name for the created {@link VoiceChannel}.
     *
     * @param name The name of the voice channel.
     * @return This mono.
     */
    public VoiceChannelCreateMono withName(String name) {
        return withBuilder(it -> it.name(name));
    }

    /**
     * Sets the bitrate for the created {@link VoiceChannel}.
     *
     * @param bitrate The maximum amount of bits to send per second in the voice channel, related to the quality of
     *                audio. A valid bitrate is a number from 8 to 96.
     * @return This mono.
     */
    public VoiceChannelCreateMono withBitrate(int bitrate) {
        return withBuilder(it -> it.bitrate(bitrate));
    }

    /**
     * Sets the user limit for the created {@link VoiceChannel}.
     * <p>
     * Users with {@link Permission#MOVE_MEMBERS} ignore this limit and can also move other users into the channel
     * past the limit.
     *
     * @param userLimit The maximum number of users that can join the voice channel at once.
     * @return This mono.
     */
    public VoiceChannelCreateMono withUserLimit(int userLimit) {
        return withBuilder(it -> it.userLimit(userLimit));
    }

    /**
     * Sets the position for the created {@link VoiceChannel}.
     *
     * @param position The raw position for the channel.
     * @return This mono.
     */
    public VoiceChannelCreateMono withPosition(int position) {
        return withBuilder(it -> it.position(position));
    }

    /**
     * Sets the permission overwrites for the created {@link VoiceChannel}.
     *
     * @param permissionOverwrites The {@code Set<PermissionOverwrite>} which contains overwrites for the channel.
     * @return This mono.
     */
    public VoiceChannelCreateMono withPermissionOverwrites(Set<? extends PermissionOverwrite> permissionOverwrites) {
        return withBuilder(it -> it.permissionOverwrites(
            permissionOverwrites.stream()
                .map(o -> OverwriteData.builder()
                    .id(o.getTargetId().asString())
                    .type(o.getType().getValue())
                    .allow(o.getAllowed().getRawValue())
                    .deny(o.getDenied().getRawValue())
                    .build())
                .collect(Collectors.toList())
        ));
    }

    /**
     * Sets the parent ID for the created {@link VoiceChannel}.
     * <p>
     * The parent ID is equivalent to a {@link Category} ID.
     *
     * @param parentId The {@code Snowflake} of the parent {@code Category}.
     * @return This mono.
     */
    public VoiceChannelCreateMono withParentId(@Nullable Snowflake parentId) {
        return withBuilder(it -> it.parentId(parentId == null ? Possible.absent() : Possible.of(parentId.asString())));
    }

    @Override
    Mono<VoiceChannel> getRequest() {
        return Mono.defer(() -> gateway.getRestClient().getGuildService()
            .createGuildChannel(guildId, requestBuilder.get().build(), reason))
            .map(data -> EntityUtil.getChannel(gateway, data))
            .cast(VoiceChannel.class);
    }
}