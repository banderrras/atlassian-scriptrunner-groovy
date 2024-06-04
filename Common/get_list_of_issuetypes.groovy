import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.config.IssueTypeManager
import com.epam.SRProperties
import com.epam.Helper
import static org.apache.commons.lang.StringEscapeUtils.escapeHtml

def props = SRProperties.instance.properties
final String confluenceBaseUrl = "https://confluence.test.net"
final String confluenceToken = props["confl_token"].toString()
final int confluencePageId = 419239654
final String confluenceTitle = "Jira Issue Types"

IssueTypeManager issueTypeManager = ComponentAccessor.getComponentOfType(IssueTypeManager.class)

StringBuilder output = new StringBuilder("<table><th>Category</th><th>Issue Type</th><th>Description</th>")

def issuetypes = issueTypeManager.getIssueTypes()
def sortedByType = issuetypes.toSorted() {a,b -> a.isSubTask() <=> b.isSubTask() ?: a.name <=> b.name }


for(def issueType: sortedByType)
{
    if(issueType.isSubTask())
        output.append("<tr><td>Sub-Task</td>")
    else
        output.append("<tr><td>Standard</td>")

    output.append("<td>${escapeHtml(issueType.name)}</td>")
    output.append("<td>${escapeHtml(issueType.description)}</td></tr>")
}

output.append("</table>")

int version = Helper.GetPageCurrentVersion(confluenceBaseUrl, confluenceToken, confluencePageId)
if(version != 0)
    log.warn(Helper.SendTableToConfluence(confluenceBaseUrl, confluenceToken, confluencePageId, confluenceTitle, output.toString(), version))