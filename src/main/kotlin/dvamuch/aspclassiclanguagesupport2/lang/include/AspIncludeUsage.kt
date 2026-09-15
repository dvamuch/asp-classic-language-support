package dvamuch.aspclassiclanguagesupport2.lang.include

import com.intellij.usageView.UsageInfo
import com.intellij.usages.UsageInfo2UsageAdapter
import com.intellij.usages.impl.rules.UsageType
import com.intellij.usages.impl.rules.UsageWithType
import java.util.function.Supplier

class AspIncludeUsage(reference: AspIncludeReference) :
    UsageInfo2UsageAdapter(UsageInfo(reference)),
    UsageWithType {

    override fun getUsageType(): UsageType = aspIncludeUsageType

    companion object {
        private val aspIncludeUsageType by lazy {
            UsageType(Supplier { "ASP include" })
        }
    }
}
