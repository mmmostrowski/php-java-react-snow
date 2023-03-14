package techbit.snow.proxy.service.stream;

import org.junit.Rule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class NamedPipesTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Mock
    NamedPipes namedPipes;


    @Test
    void whenDestroyingAllPipes_thenFolderWithPipesMustBePurged() throws IOException {
        folder.create();
        folder.newFile();
        folder.newFolder();
        folder.newFile();

        Mockito.when(namedPipes.pipesDir()).thenReturn(folder.getRoot().toPath());
        Mockito.doCallRealMethod().when(namedPipes).destroyAll();

        namedPipes.destroyAll();

        try (Stream<Path> stream = Files.list(folder.getRoot().toPath())) {
            assertEquals(0, stream.count());
        }
    }
    
    @Test 
    void whenAskedForPipesDir_thenValidDirectoryPathProvided() throws IOException {
        Mockito.doCallRealMethod().when(namedPipes).pipesDir();

        assertDoesNotThrow(() -> namedPipes.pipesDir().toRealPath());
    }

}