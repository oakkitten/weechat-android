package androidx.preference

import android.content.Context
import android.util.AttributeSet
import android.widget.TextView
import com.ubergeek42.WeechatAndroid.R
import com.ubergeek42.WeechatAndroid.media.Config
import com.ubergeek42.cats.Cat

class StrategyPreference(context: Context, attrs: AttributeSet?) :
        FullScreenEditTextPreference(context, attrs) {

    @Cat(exit = true) override fun getSummary(): CharSequence {
        val info = Config.parseConfigSafe(text)
                ?: return context.getString(R.string.pref__StrategyPreference__summary_error)

        val messageFilter = if (info.messageFilter != null)
            context.getString(R.string.pref__StrategyPreference__message_filter_set) else
            context.getString(R.string.pref__StrategyPreference__message_filter_not_set)

        val lineFilters = if (info.lineFilters == null)
            context.getString(R.string.pref__StrategyPreference__0_line_filters_set) else
            context.resources.getQuantityString(
                R.plurals.pref__StrategyPreference__n_line_filters_set,
                info.lineFilters.size,
                info.lineFilters.size
            )

        val summaries = if (info.strategies == null) {
            context.getString(R.string.pref__StrategyPreference__strategies_not_loaded)
        } else {
            val commaSeparatedNames = info.strategies.joinToString { it.name }
            context.getString(R.string.pref__StrategyPreference__strategies_list, commaSeparatedNames)
        }

        return context.getString(R.string.pref__StrategyPreference__summary,
                                 messageFilter,
                                 lineFilters,
                                 summaries)
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        val summary = holder.findViewById(android.R.id.summary) as TextView
        summary.setMaxHeight(Int.MAX_VALUE)
    }
}
