package pt.hitv.core.model


class CategoryWithTvShow (val category: Category, val tvShows: List<TvShow>){

    fun toCategoryWithTvShows(): CategoryWithTvShow {
        return CategoryWithTvShow(category, tvShows)
    }

    companion object {

        fun from(category: Category,tvShows: List<TvShow>): CategoryWithTvShow {
            return CategoryWithTvShow(category,tvShows)
        }
    }
}
