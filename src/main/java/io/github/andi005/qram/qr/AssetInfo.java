package io.github.andi005.qram.qr;

/** What the JSON lookup returns about a scanned asset. */
public record AssetInfo(
    long id,
    String tag,
    String name,
    String status,
    String statusLabel,
    String location,
    String holder,
    String url) {}
