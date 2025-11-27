package org.example.service;

import java.io.File;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class RecentActionsService {

    
    public static final int LIMIT = 10;

    
    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());

    public static final class Segment {
        public final double startSec, endSec;
        public final Instant at;
        public Segment(double startSec, double endSec, Instant at) {
            this.startSec = startSec; this.endSec = endSec; this.at = at;
        }
        @Override public String toString() {
            return String.format("[%.2f .. %.2f] (%s)", startSec, endSec, TIME_FMT.format(at));
        }
    }

    public static final class Project {
        public final String name;
        public final Instant at;
        public Project(String name, Instant at) {
            this.name = name; this.at = at;
        }
        @Override public String toString() {
            return String.format("%s (%s)", name, TIME_FMT.format(at));
        }
    }

    private final Deque<Path>     files    = new ArrayDeque<>();
    private final Deque<Segment>  segments = new ArrayDeque<>();
    private final Deque<Project>  projects = new ArrayDeque<>();

    
    public void addFile(Path p)                 { pushLimited(files, p); }
    public void addFile(File f)                 { if (f != null) addFile(f.toPath()); }
    public void addSegment(double s, double e)  { pushLimited(segments, new Segment(s, e, Instant.now())); }
    public void addProject(String name)         { if (name != null && !name.isEmpty()) pushLimited(projects, new Project(name, Instant.now())); }

    
    public List<Path> files()       { return new ArrayList<>(files); }
    public List<Segment> segments() { return new ArrayList<>(segments); }
    public List<Project> projects() { return new ArrayList<>(projects); }

    
    public List<String> filesDisplay() {
        List<String> out = new ArrayList<>();
        for (Path p : files) out.add(p.getFileName().toString());
        return out;
    }
    public List<String> segmentsDisplay() {
        List<String> out = new ArrayList<>();
        for (Segment s : segments) out.add(s.toString());
        return out;
    }
    public List<String> projectsDisplay() {
        List<String> out = new ArrayList<>();
        for (Project pr : projects) out.add(pr.toString());
        return out;
    }

    
    public void clear() {
        files.clear();
        segments.clear();
        projects.clear();
    }

    private static <T> void pushLimited(Deque<T> dq, T v) {
        dq.addFirst(v);
        while (dq.size() > LIMIT) dq.removeLast();
    }
}
