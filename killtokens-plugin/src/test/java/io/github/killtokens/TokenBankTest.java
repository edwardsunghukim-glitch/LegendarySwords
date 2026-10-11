package io.github.killtokens;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class TokenBankTest {
    @TempDir Path dir;
    @Test void balancesPersistAndNegativeWithdrawalsAreRejected() throws Exception {
        Path path=dir.resolve("balances.yml");UUID id=UUID.randomUUID();TokenBank bank=new TokenBank(path);
        assertTrue(bank.change(id,10));assertTrue(bank.change(id,-3));assertFalse(bank.change(id,-8));
        assertEquals(7,new TokenBank(path).balance(id));
    }
    @Test void overflowIsRejectedWithoutChangingBalance() throws Exception {
        TokenBank bank=new TokenBank(dir.resolve("balances.yml"));UUID id=UUID.randomUUID();
        assertTrue(bank.change(id,Long.MAX_VALUE));assertFalse(bank.change(id,1));assertEquals(Long.MAX_VALUE,bank.balance(id));
    }
    @Test void multiplierPersistsAndExpiresAtDeadline() throws Exception {
        Path path=dir.resolve("balances.yml");TokenBank bank=new TokenBank(path);bank.activate(3,1000);
        TokenBank loaded=new TokenBank(path);assertEquals(3,loaded.activeMultiplier(999));assertEquals(1,loaded.activeMultiplier(1000));
    }
    @Test void failedWriteRollsBackBalance() throws Exception {
        Path path=dir.resolve("balances.yml");TokenBank bank=new TokenBank(path);UUID id=UUID.randomUUID();
        Files.createDirectory(path);Files.writeString(path.resolve("keep"),"not an account file");
        assertThrows(java.io.IOException.class,()->bank.change(id,5));assertEquals(0,bank.balance(id));
    }
    @Test void invalidBankFileFailsInsteadOfResettingWallets() throws Exception {
        Path path=dir.resolve("balances.yml");Files.writeString(path,"balances: [broken yaml");
        assertThrows(org.bukkit.configuration.InvalidConfigurationException.class,()->new TokenBank(path));
    }
}
