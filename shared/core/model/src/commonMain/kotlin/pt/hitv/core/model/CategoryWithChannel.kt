package pt.hitv.core.model


class CategoryWithChannel (val category: Category, val channels: List<Channel>){

    fun toCategoryWithChannels(): CategoryWithChannel {
        return CategoryWithChannel(category, channels)
    }

    companion object {

        fun from(category: Category,channels: List<Channel>): CategoryWithChannel {
            return CategoryWithChannel(category,channels)
        }
    }
}
