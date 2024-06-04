import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.config.StatusManager
import com.epam.SRProperties
import com.epam.Helper
import static org.apache.commons.lang.StringEscapeUtils.escapeHtml

def props = SRProperties.instance.properties
final String confluenceBaseUrl = "https://confluence.test.net"
final String confluenceToken = props["confl_token"].toString()
final int confluencePageId = 416385409
final String confluenceTitle = "Jira Statuses"


StatusManager statusManager = ComponentAccessor.getComponentOfType(StatusManager.class)

StringBuilder output = new StringBuilder("<table><th>Category</th><th>Status</th>")
def statuses = statusManager.getStatuses()
def sortedByCat = statuses.toSorted { a, b -> a.getStatusCategory().getName() <=> b.getStatusCategory().getName() ?: a.name <=> b.name }


for(def status: sortedByCat)
{
    def category = status.getStatusCategory().getName()

    switch(category)
    {
        case "Complete":
            output.append("<tr><td style='background-color:green;color:white;'>")
            break
        case "In Progress":
            output.append("<tr><td style='background-color:blue;color:white;'>")
            break
        case "New":
            output.append("<tr><td style='background-color:gray;color:white;'>")
            break
        default:
            output.append("<tr><td>")
            break
    }

    output.append("$category</td><td>${escapeHtml(status.name)}</td></tr>")
}

output.append("</table>")


int version = Helper.GetPageCurrentVersion(confluenceBaseUrl, confluenceToken, confluencePageId)
if(version != 0)
    log.warn(Helper.SendTableToConfluence(confluenceBaseUrl, confluenceToken, confluencePageId, confluenceTitle, output.toString(), version))