package cn.nicolite.huthelper.kbase

import android.support.v7.widget.RecyclerView
import com.github.jdsjlzx.recyclerview.LRecyclerViewAdapter

/**
 * Created by nicolite on 2019/3/6.
 * email nicolite@nicolite.cn
 * 返回LRecyclerViewAdapter
 */
abstract class BaseLAdapter<T : RecyclerView.ViewHolder>
    : RecyclerView.Adapter<T>() {
    fun getLAdapter(): LRecyclerViewAdapter = LRecyclerViewAdapter(this)
}