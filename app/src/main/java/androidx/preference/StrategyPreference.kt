package androidx.preference

import android.content.Context
import android.util.AttributeSet
import android.widget.TextView
import com.ubergeek42.WeechatAndroid.R
import com.ubergeek42.WeechatAndroid.media.Config
import com.ubergeek42.WeechatAndroid.utils.Utils
import com.ubergeek42.cats.Cat

class StrategyPreference(context: Context?, attrs: AttributeSet?) :
        FullScreenEditTextPreference(context, attrs) {
    @Cat(exit = true) override fun getSummary(): CharSequence? {
        val context = getContext()

        val info = Config.parseConfigSafe(getText())
        if (info == null) return context.getString(R.string.pref__StrategyPreference__summary_error)

        val messageFilter =
            if (info.messageFilter != null) context.getString(R.string.pref__StrategyPreference__message_filter_set) else context.getString(
                R.string.pref__StrategyPreference__message_filter_not_set)

        val lineFilters =
            if (info.lineFilters == null) context.getString(R.string.pref__StrategyPreference__0_line_filters_set) else context.getResources()
                    .getQuantityString(R.plurals.pref__StrategyPreference__n_line_filters_set,
                                       info.lineFilters!!.size, info.lineFilters!!.size)

        val summaries: String?
        if (info.strategies == null) {
            summaries = context.getString(R.string.pref__StrategyPreference__strategies_not_loaded)
        } else {
            val names: MutableList<CharSequence?> = ArrayList<CharSequence?>()
            for (s in info.strategies) names.add(s.getName())
            summaries = context.getString(R.string.pref__StrategyPreference__strategies_list,
                                          Utils.join(", ", names))
        }

        return context.getString(R.string.pref__StrategyPreference__summary,
                                 messageFilter,
                                 lineFilters,
                                 summaries)
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        val summary = holder.findViewById(android.R.id.summary) as TextView
        summary.setMaxHeight(Int.Companion.MAX_VALUE)
    }
}
