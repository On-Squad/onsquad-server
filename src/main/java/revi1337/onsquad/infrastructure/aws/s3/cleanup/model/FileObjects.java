package revi1337.onsquad.infrastructure.aws.s3.cleanup.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public class FileObjects {

    private final List<FileObject> fileObjects;

    public FileObjects(List<FileObject> fileObjects) {
        this.fileObjects = Collections.unmodifiableList(fileObjects);
    }

    public boolean isEmpty() {
        return fileObjects.isEmpty();
    }

    public boolean isNotEmpty() {
        return !isEmpty();
    }

    public int size() {
        return fileObjects.size();
    }

    public List<FileObjects> partition(int size) {
        List<FileObjects> partitions = new ArrayList<>();
        for (int i = 0; i < fileObjects.size(); i += size) {
            List<FileObject> partition = fileObjects.subList(i, Math.min(i + size, fileObjects.size()));
            partitions.add(new FileObjects(partition));
        }
        return partitions;
    }

    public FileObjects filterByPaths(List<String> paths) {
        Set<String> pathSet = new HashSet<>(paths);
        List<FileObject> filtered = fileObjects.stream()
                .filter(fp -> pathSet.contains(fp.getPath()))
                .toList();

        return new FileObjects(filtered);
    }

    public List<Long> getFileIds() {
        return fileObjects.stream()
                .map(FileObject::getId)
                .toList();
    }

    public List<String> pathValues() {
        return fileObjects.stream()
                .map(FileObject::getPath)
                .toList();
    }

    public long getLastFileId() {
        if (fileObjects.isEmpty()) {
            return 0L;
        }

        return fileObjects.get(fileObjects.size() - 1).getId();
    }

    public List<FileObject> values() {
        return fileObjects;
    }

    public Stream<FileObject> stream() {
        return fileObjects.stream();
    }
}
