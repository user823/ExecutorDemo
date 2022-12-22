import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.*;


public class ExecutorDemo {

    //统计单个文件中单词出现的次数
    public static long occurrences(String word, Path path)
    {
        try (var in = new Scanner(path))
        {
            int count = 0;
            while (in.hasNext())
            {
                if (in.next().equals(word)) count++;
            }
            return count;
        }
        catch (IOException e)
        {
            return 0;
        }
    }

    public static Set<Path> descendants(Path rootDir) throws IOException
    {
        try (Stream<Path> entries = Files.walk(rootDir))
        {
            return entries.filter(Files::isRegularFile).collect(Collectors.toSet());
        }
    }

    //创建Callable对象：判断文件中是否包含word
    public static Callable<Path> searchForTask(String word, Path path)
    {
        return () ->
        {
            try (var in = new Scanner(path))
            {
                while (in.hasNext())
                {
                    if (in.next().equals(word)) return path;
                    if (Thread.currentThread().isInterrupted())
                    {
                        System.out.println("Search in " + path + " canceled.");
                        return null;
                    }
                }
                throw new NoSuchElementException();
            }
        };
    }

    public static void main(String[] args) throws InterruptedException, ExecutionException, IOException
    {
        try (var in = new Scanner(System.in))
        {
            System.out.print("Enter base directory (e.g. /opt/jdk-9-src): ");
            String start = in.nextLine();
            System.out.print("Enter keyword (e.g. volatile): ");
            String word = in.nextLine();

            Set<Path> files = descendants(Path.of(start));
            var tasks = new ArrayList<Callable<Long>>();
            for (Path file : files)
            {
                Callable<Long> task = () -> occurrences(word, file);
                tasks.add(task);
            }
            ExecutorService executor = Executors.newCachedThreadPool();
            Instant startTime = Instant.now();
            List<Future<Long>> results = executor.invokeAll(tasks);
            long total = 0;
            for (var result : results)
            {
                total += result.get();
            }
            Instant endTime = Instant.now();
            System.out.println("Occurrences of " + word + ": " + total);
            System.out.println("Time elapsed: "
            + Duration.between(startTime, endTime).toMillis() + "ms");

            var searchTasks = new ArrayList<Callable<Path>>();
            for (Path file : files)
            {
                searchTasks.add(searchForTask(word, file));
            }
            Path found = executor.invokeAny(searchTasks);
            System.out.println(word + " occurs in:" + found);

            if (executor instanceof ThreadPoolExecutor tpExecutor)
            {
                System.out.println("Largest pool size:" + tpExecutor.getLargestPoolSize());
            }
            executor.shutdown();
        }
    }
}
