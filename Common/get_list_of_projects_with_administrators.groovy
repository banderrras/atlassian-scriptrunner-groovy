import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.project.Project
import com.atlassian.jira.project.ProjectManager
import com.atlassian.jira.security.roles.ProjectRole
import com.atlassian.jira.security.roles.ProjectRoleActors
import com.atlassian.jira.security.roles.ProjectRoleManager
import com.epam.SRProperties
import com.epam.Helper
import static org.apache.commons.lang.StringEscapeUtils.escapeHtml
import com.atlassian.jira.config.properties.APKeys

final String projectRole = "Administrators"
final String adminGroupName = "jira-administrators"
def props = SRProperties.instance.properties
final String confluenceBaseUrl = "https://confluence.test.net"
final String confluenceToken = props["confl_token"].toString()
final int confluencePageId = 424134568
final String confluenceTitle = "Jira Projects"

final String baseurl = ComponentAccessor.getApplicationProperties().getString(APKeys.JIRA_BASEURL)


def projectManager = ComponentAccessor.getProjectManager()
def projectRoleManager = ComponentAccessor.getComponent(ProjectRoleManager)


//def project = projectManager.getProjectByCurrentKey(projectKey)
def adminGroup = ComponentAccessor.groupManager.getGroup(adminGroupName)
ProjectRole projectRoleObj = projectRoleManager.getProjectRole(projectRole)

def prList = projectManager.getProjectObjects()

StringBuilder output = new StringBuilder("<table><th>Project</th><th>Project Lead</th><th>Administrators</th>")

prList.each { project ->

    output.append("<tr><td>${escapeHtml(project.name)} (<a href='$baseurl/projects/$project.key/summary'>$project.key</a>)</td>")

    ProjectRoleActors actors = projectRoleManager.getProjectRoleActors(projectRoleObj, project)
    def users = actors.getUsers()

    def projectLead = project.getProjectLead()

    if(projectLead)
        output.append("<td>${escapeHtml(project.getProjectLead().getDisplayName())} (${escapeHtml(project.getProjectLead().getEmailAddress())})</td>")
    else
        output.append("<td>Unknown</td>")
        
    output.append("<td>")

    for(def user: users)
    {
        if(!ComponentAccessor.groupManager.isUserInGroup(user,adminGroup))
        {
            output.append(escapeHtml(user.getDisplayName()) + " (" + escapeHtml(user.getEmailAddress()) + ")<br />")
        }
    }
    output.append("</td></tr>")
}

output.append("</table>")


int version = Helper.GetPageCurrentVersion(confluenceBaseUrl, confluenceToken, confluencePageId)
if(version != 0)
    log.warn(Helper.SendTableToConfluence(confluenceBaseUrl, confluenceToken, confluencePageId, confluenceTitle, output.toString(), version))

