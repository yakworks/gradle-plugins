package yakworks.gradle.util.team;

public class TeamMember {
    public final String gitHubUser;
    public final String name;
    TeamMember(String gitHubUser, String name) {
        this.gitHubUser = gitHubUser;
        this.name = name;
    }
}
