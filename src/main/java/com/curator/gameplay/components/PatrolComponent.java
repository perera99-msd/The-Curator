package com.curator.gameplay.components;

import com.almasb.fxgl.entity.component.Component;
import javafx.geometry.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

public class PatrolComponent extends Component {

    private final List<Point2D> path = new ArrayList<>();
    private final double speed;
    private final Predicate<Point2D> canOccupy;
    private int targetIndex = 1;

    public PatrolComponent(double speed, Point2D... points) {
        this(speed, p -> true, points);
    }

    public PatrolComponent(double speed, Predicate<Point2D> canOccupy, Point2D... points) {
        this.speed = speed;
        this.canOccupy = canOccupy;
        this.path.addAll(Arrays.asList(points));
    }

    @Override
    public void onAdded() {
        if (path.isEmpty()) {
            path.add(new Point2D(entity.getX(), entity.getY()));
        }
        if (path.size() == 1) {
            path.add(path.get(0));
        }
        Point2D first = path.get(0);
        if (canOccupy.test(first)) {
            entity.setPosition(first);
        } else {
            path.set(0, new Point2D(entity.getX(), entity.getY()));
        }
    }

    @Override
    public void onUpdate(double tpf) {
        if (path.size() < 2) return;

        var target = path.get(targetIndex);
        var current = new Point2D(entity.getX(), entity.getY());
        var delta = target.subtract(current);
        var distance = delta.magnitude();

        // Target angle based on current movement direction
        double targetAngle = Math.toDegrees(Math.atan2(delta.getY(), delta.getX()));
        double currentAngle = entity.getRotation();

        // Smooth rotation
        double diff = targetAngle - currentAngle;
        while (diff < -180) diff += 360;
        while (diff > 180) diff -= 360;
        
        entity.setRotation(currentAngle + diff * 5.0 * tpf);

        // If we reached the waypoint
        if (distance < speed * tpf) {
            entity.setPosition(target);
            targetIndex = (targetIndex + 1) % path.size();
            return;
        }

        var step = delta.normalize().multiply(speed * tpf);
        Point2D next = current.add(step);
        if (canOccupy.test(next)) {
            entity.setPosition(next);
        } else {
            targetIndex = (targetIndex + 1) % path.size();
        }
    }
}
