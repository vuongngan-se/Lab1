package com.vku.chat.model;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public class Room implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String name;
    private String description;
    private Set<String> members = new HashSet<>();
    private boolean isMulticastRoom;
    private String multicastIp;
    private int multicastPort;

    public Room() {}

    public Room(String id, String name, String description, boolean isMulticastRoom, String multicastIp, int multicastPort) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.isMulticastRoom = isMulticastRoom;
        this.multicastIp = multicastIp;
        this.multicastPort = multicastPort;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Set<String> getMembers() { return members; }
    public void setMembers(Set<String> members) { this.members = members; }

    public boolean isMulticastRoom() { return isMulticastRoom; }
    public void setMulticastRoom(boolean multicastRoom) { isMulticastRoom = multicastRoom; }

    public String getMulticastIp() { return multicastIp; }
    public void setMulticastIp(String multicastIp) { this.multicastIp = multicastIp; }

    public int getMulticastPort() { return multicastPort; }
    public void setMulticastPort(int multicastPort) { this.multicastPort = multicastPort; }

    public void addMember(String username) { members.add(username); }
    public void removeMember(String username) { members.remove(username); }
    public boolean hasMember(String username) { return members.contains(username); }

    @Override
    public String toString() {
        return name + " (" + members.size() + " thành viên)";
    }
}
