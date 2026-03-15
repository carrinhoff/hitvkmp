package pt.hitv.core.model


class CategoryWithMovie (val category: Category, val movies: List<Movie>){

    fun toCategoryWithMovies(): CategoryWithMovie {
        return CategoryWithMovie(category, movies)
    }

    companion object {

        fun from(category: Category,movies: List<Movie>): CategoryWithMovie {
            return CategoryWithMovie(category,movies)
        }
    }
}
