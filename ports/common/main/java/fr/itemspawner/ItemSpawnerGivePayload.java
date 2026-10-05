package fr.itemspawner;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.LinkedHashMap;
import java.util.Map;

public record ItemSpawnerGivePayload(
        Identifier itemId,
        int quantity,
        Map<Identifier, Integer> enchantments,
        boolean mine3x3
) implements CustomPacketPayload {
    public static final Type<ItemSpawnerGivePayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath("itemspawner", "give_item")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, ItemSpawnerGivePayload> STREAM_CODEC = StreamCodec.of(
            ItemSpawnerGivePayload::encode,
            ItemSpawnerGivePayload::decode
    );

    public ItemSpawnerGivePayload {
        enchantments = Map.copyOf(enchantments);
    }

    private static void encode(RegistryFriendlyByteBuf buffer, ItemSpawnerGivePayload payload) {
        buffer.writeIdentifier(payload.itemId());
        buffer.writeVarInt(payload.quantity());
        int count = Math.min(64, payload.enchantments().size());
        buffer.writeVarInt(count);
        int written = 0;
        for (Map.Entry<Identifier, Integer> entry : payload.enchantments().entrySet()) {
            if (written++ >= count) {
                break;
            }
            buffer.writeIdentifier(entry.getKey());
            buffer.writeVarInt(entry.getValue());
        }
        buffer.writeBoolean(mine3x3);
    }

    private static ItemSpawnerGivePayload decode(RegistryFriendlyByteBuf buffer) {
        Identifier itemId = buffer.readIdentifier();
        int quantity = buffer.readVarInt();
        int count = Math.max(0, Math.min(64, buffer.readVarInt()));
        Map<Identifier, Integer> enchantments = new LinkedHashMap<>();
        for (int index = 0; index < count; index++) {
            enchantments.put(buffer.readIdentifier(), buffer.readVarInt());
        }
        return new ItemSpawnerGivePayload(itemId, quantity, enchantments, buffer.readBoolean());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}