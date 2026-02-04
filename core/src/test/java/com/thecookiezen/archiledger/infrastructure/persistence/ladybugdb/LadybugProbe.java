
import com.ladybugdb.Connection;
import java.lang.reflect.Method;

public class LadybugProbe {
    public static void main(String[] args) {
        System.out.println("Methods of com.ladybugdb.Connection:");
        for (Method m : Connection.class.getMethods()) {
            System.out.println(m.toString());
        }
    }
}
