package xyz.fmdc.arw;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import xyz.fmdc.arw.api.projectile.telemetry.FlightTelemetryLogger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class FlightTelemetryLoggerTest {

    @Test
    public void testTelemetryRowFormatting() {
        FlightTelemetryLogger.TelemetryRow row = new FlightTelemetryLogger.TelemetryRow(
                1, 0.05,
                new Vec3(10.1234, 64.5678, 20.9012),
                new Vec3(50.0, 10.5, 0.0),
                51.09,
                new Vec3(1.0, 0.0, 0.0),
                2.35,
                11.85f,
                0.0f,
                1.225,
                28000.0,
                125.43,
                15.20,
                ""
        );

        String csv = row.toCsvLine();
        assertNotNull(csv);
        String[] tokens = csv.split(",", -1);
        assertEquals(20, tokens.length, "20個のカラムが出力されていること");
        assertEquals("1", tokens[0]);
        assertEquals("0.05", tokens[1]);
        assertEquals("10.123", tokens[2]);
        assertEquals("64.568", tokens[3]);
        assertEquals("20.901", tokens[4]);
        assertEquals("51.09", tokens[8]);
        assertEquals("2.35", tokens[12]);
        assertEquals("125.43", tokens[17]);
        assertEquals("15.20", tokens[18]);
        assertEquals("", tokens[19]);
    }

    @Test
    public void testEndSessionFlushesCsv() throws Exception {
        UUID testUuid = UUID.randomUUID();
        FlightTelemetryLogger.FlightSession session = new FlightTelemetryLogger.FlightSession(testUuid, "TestShell");

        // 発射
        session.addRow(new FlightTelemetryLogger.TelemetryRow(
                0, 0.0,
                new Vec3(0, 64, 0), new Vec3(100, 0, 0), 100.0,
                new Vec3(1, 0, 0), 0.0, 0.0f, 0.0f,
                1.225, 0.0, 0.0, 0.0, "LAUNCH"
        ));

        // 1Tick後
        session.addRow(new FlightTelemetryLogger.TelemetryRow(
                1, 0.05,
                new Vec3(5, 63.9, 0), new Vec3(99.5, -0.49, 0), 99.5,
                new Vec3(1, 0, 0), 0.28, -0.28f, 0.0f,
                1.225, 0.0, 45.2, 0.8, ""
        ));

        List<FlightTelemetryLogger.TelemetryRow> rows = session.snapshotAndEnd();
        assertEquals(2, rows.size());
        assertTrue(session.isEnded());

        // 終了レコードの追加
        rows.add(new FlightTelemetryLogger.TelemetryRow(
                2, 0.10,
                new Vec3(10, 63.5, 0), new Vec3(99.0, -0.98, 0), 99.0,
                new Vec3(1, 0, 0), 0.50, -0.50f, 0.0f,
                1.225, 0.0, 0.0, 0.0, "HIT_BLOCK [10, 63, 0]"
        ));

        assertEquals(3, rows.size());
        assertEquals("HIT_BLOCK [10, 63, 0]", rows.get(2).event());
    }
}
