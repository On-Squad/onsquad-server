package revi1337.onsquad.infrastructure.aws.s3.cleanup.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public class FilePaths {

    private final List<FilePath> filePaths;

    public FilePaths(List<FilePath> filePaths) {
        this.filePaths = Collections.unmodifiableList(filePaths);
    }

    public boolean isEmpty() {
        return filePaths.isEmpty();
    }

    public boolean isNotEmpty() {
        return !isEmpty();
    }

    public int size() {
        return filePaths.size();
    }

    public List<FilePaths> partition(int size) {
        List<FilePaths> partitions = new ArrayList<>();
        for (int i = 0; i < filePaths.size(); i += size) {
            List<FilePath> partition = filePaths.subList(i, Math.min(i + size, filePaths.size()));
            partitions.add(new FilePaths(partition));
        }
        return partitions;
    }

    public FilePaths filterByPaths(List<String> paths) {
        Set<String> pathSet = new HashSet<>(paths);
        List<FilePath> filtered = filePaths.stream()
                .filter(fp -> pathSet.contains(fp.getPath()))
                .toList();

        return new FilePaths(filtered);
    }

    public List<Long> getFileIds() {
        return filePaths.stream()
                .map(FilePath::getId)
                .toList();
    }

    public List<String> pathValues() {
        return filePaths.stream()
                .map(FilePath::getPath)
                .toList();
    }

    public long getLastFileId() {
        if (filePaths.isEmpty()) {
            return 0L;
        }

        return filePaths.get(filePaths.size() - 1).getId();
    }

    public List<FilePath> values() {
        return filePaths;
    }

    public Stream<FilePath> stream() {
        return filePaths.stream();
    }
}
