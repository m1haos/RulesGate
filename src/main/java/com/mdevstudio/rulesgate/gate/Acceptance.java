package com.mdevstudio.rulesgate.gate;

import java.time.Instant;

public record Acceptance(int version, Instant acceptedAt) {
}
