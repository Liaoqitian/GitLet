package gitlet;

import java.io.IOException;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;

/* Driver class for Gitlet, the tiny stupid version-control system.
   @author
*/
public class Main {

    /* Usage: java gitlet.Main ARGS, where ARGS contains
       <COMMAND> <OPERAND> .... */
    public static void main(String... args) throws IOException {
        try {
            Command command = new Command(args);
            Container container = loadContainer();
            container = command.execute(container);
            if (container != null) {
                saveContainer(container);
            }
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
        }
    }

    private static void saveContainer(Container container) {
        try (ObjectOutputStream out = new ObjectOutputStream(
                new FileOutputStream(Command.containerDir))) {
            out.writeObject(container);
        } catch (IOException e) {
            return;
        }
    }

    private static Container loadContainer() {
        Container container = null;
        try (ObjectInputStream in =
                     new ObjectInputStream(new FileInputStream(Command.containerDir))) {
            container = (Container) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            container = null;
        }
        return container;
    }
}
