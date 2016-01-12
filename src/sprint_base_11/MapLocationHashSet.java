package sprint_base_11;

import battlecode.common.*;

public class MapLocationHashSet {

	public void clear() {
		size = 0;
		locations = new MapLocation[10000];
		hasLocation = new boolean[100][100];
	}
	
	public int size = 0;
	public MapLocation[] locations = new MapLocation[10000];
	public boolean[][] hasLocation = new boolean[100][100];
	
	public boolean contains(MapLocation loc) {
		int x = (loc.x + 32000) % 100;
		int y = (loc.y + 32000) % 100;
		return hasLocation[x][y];
	}
	
	// returns true if the given location was added
	// (that is, if it wasn't already in the set)
	public boolean add(MapLocation loc) {
		int x = (loc.x + 32000) % 100;
		int y = (loc.y + 32000) % 100;
		if (!hasLocation[x][y]) {
			hasLocation[x][y] = true;
			locations[size] = loc;
			size += 1;
			return true;
		} else {
			return false;
		}
	}
	
	public boolean remove(MapLocation loc) {
		int x = (loc.x + 32000) % 100;
		int y = (loc.y + 32000) % 100;
		if (hasLocation[x][y]) {
			hasLocation[x][y] = false;
			size -= 1;
			for (int i = 0; i < size; ++i) {
				if (locations[i].equals(loc)) {
					locations[i] = locations[size];
					break;
				}
			}
			return true;
 		} else {
			return false;
		}
	}
}