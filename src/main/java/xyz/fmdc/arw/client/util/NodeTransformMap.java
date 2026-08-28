package xyz.fmdc.arw.client.util;

import java.util.HashMap;
import java.util.Map;

public class NodeTransformMap {
    public static class NodeTransform {
        public float yaw = 0.0f;
        public float pitch = 0.0f;
        public float roll = 0.0f;

        public NodeTransform setYaw(float yaw) { this.yaw = yaw; return this; }
        public NodeTransform setPitch(float pitch) { this.pitch = pitch; return this; }
        public NodeTransform setRoll(float roll) { this.roll = roll; return this; }
    }

    private final Map<String, NodeTransform> transforms = new HashMap<>();

    public NodeTransform getOrCreate(String nodeName) {
        return transforms.computeIfAbsent(nodeName, k -> new NodeTransform());
    }

    public NodeTransform get(String nodeName) {
        return transforms.get(nodeName);
    }
}