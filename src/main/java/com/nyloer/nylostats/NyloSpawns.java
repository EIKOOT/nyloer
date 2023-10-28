package com.nyloer.nylostats;

import lombok.Getter;
import net.runelite.api.Point;

import java.util.HashMap;

enum NyloSpawns
{
    WEST_NORTH(new Point(17, 25)),
    WEST_SOUTH(new Point(17, 24)),
    SOUTH_WEST(new Point(31, 9)),
    SOUTH_EAST(new Point(32, 9)),
    EAST_SOUTH(new Point(46, 24)),
    EAST_NORTH(new Point(46, 25)),
    EAST_BIG(new Point(46, 25)),
    WEST_BIG(new Point(17, 25)),
    SOUTH_BIG(new Point(32, 9));


    @Getter
    private final Point point;

    @Getter
    private static final HashMap<Point, NyloSpawns> lookup;
    static
    {
        lookup = new HashMap<>();
        for (NyloSpawns spawn : NyloSpawns.values())
        {
            lookup.put(spawn.getPoint(), spawn);
        }
    }

    NyloSpawns(Point point)
    {
        this.point = point;
    }
}

