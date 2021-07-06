package yakworks.gradle.util.team;

import org.gradle.api.GradleException;

import java.util.Collection;

import static yakworks.gradle.util.ArgumentValidation.notNull;

public class TeamParser {

    public static void validateTeamMembers(Collection<String> teamMembers) throws InvalidInput {
        for (String member : teamMembers) {
            parsePerson(member);
        }
    }

    public static class InvalidInput extends GradleException {
        InvalidInput(String message) {
            super(message);
        }
    }

    public static TeamMember parsePerson(String notation) throws InvalidInput {
        notNull(notation, "Team member notation cannot be null");
        String[] split = notation.split(":");
        if (split.length != 2) {
            throw invalidInput(notation);
        }
        TeamMember person = new TeamMember(split[0], split[1]);
        if (person.gitHubUser.trim().isEmpty() || person.name.trim().isEmpty()) {
            throw invalidInput(notation);
        }
        return person;
    }

    private static InvalidInput invalidInput(String notation) {
        return new InvalidInput("Invalid value of team member: '" + notation + "'" +
                "\nIt should be: 'GITHUB_USER:FULL_NAME'" +
                "\nExample of correct notation: 'basejump:Joshua Bur'");
    }
}
