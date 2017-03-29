import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Jikai Zhang on 2017/3/27.
 */
public class TestHashIncrement {

    public static void main(String[] args) {
        AtomicInteger hashCode = new AtomicInteger();

        int hash_increment = 0x61c88647;
        int size = 64;
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            list.add(hashCode.getAndAdd(hash_increment) & (size - 1));
        }
        System.out.println("original:" + list);
        Collections.sort(list);
        System.out.println("sort:    " + list);
    }



}
