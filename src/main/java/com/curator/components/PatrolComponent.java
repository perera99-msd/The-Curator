package com.curator.components;

import com.almasb.fxgl.entity.component.Component;
import javafx.geometry.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PatrolComponent extends Component {

    private final List<Point2D> path = new ArrayList<>();
    private final double speed;
    private int targetIndex = 1;

    public PatrolComponent(double speed, Point2D... points) {
        this.speed = speed;
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

        entity.setPosition(path.get(0));
    }

    @Override
    public void onUpdate(double tpf) {
        if (path.size() < 2) {
            return;
        }

        var target = path.get(targetIndex);
        var current = new Point2D(entity.getX(), entity.getY());
        var delta = target.subtract(current);
        var distance = delta.magnitude();

        if (distance < 4) {
            targetIndex = (targetIndex + 1) % path.size();
            return;
        }

        var step = delta.normalize().multiply(speed * tpf);
        entity.translate(step);

        // Keep the guard and its vision cone pointed at travel direction.
        entity.setRotation(Math.toDegrees(Math.atan2(step.getY(), step.getX())) + 90);
    }
}
