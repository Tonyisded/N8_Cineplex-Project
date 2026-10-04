package com.nhom8.cineplex.data

import com.nhom8.cineplex.R
import com.nhom8.cineplex.model.Movie
import java.text.Normalizer
import java.util.Locale

object MockMovies {
    val all = listOf(
        Movie("dune", "Dune: Phần Hai", "Dune: Part Two", 2024, "Khoa học viễn tưởng", 166, "T16", false, R.drawable.poster_dune, 259f / 384f, "Denis Villeneuve", "Timothée Chalamet, Zendaya, Rebecca Ferguson, Austin Butler", "Paul Atreides sát cánh cùng Chani và người Fremen trên hành tinh Arrakis. Giữa tình yêu và trách nhiệm, anh phải lựa chọn con đường để ngăn một tương lai khủng khiếp mà chỉ mình anh có thể nhìn thấy."),
        Movie("interstellar", "Interstellar", "Interstellar", 2014, "Khoa học viễn tưởng", 169, "T13", false, R.drawable.poster_interstellar, 500f / 750f, "Christopher Nolan", "Matthew McConaughey, Anne Hathaway, Jessica Chastain, Michael Caine", "Khi Trái Đất không còn đủ sức nuôi sống con người, Cooper cùng một nhóm phi hành gia đi qua hố sâu vũ trụ để tìm một mái nhà mới. Hành trình vượt không gian và thời gian cũng là cuộc tìm đường trở về với gia đình."),
        Movie("inception", "Inception", "Inception", 2010, "Hành động · Viễn tưởng", 148, "T16", false, R.drawable.poster_inception, 500f / 750f, "Christopher Nolan", "Leonardo DiCaprio, Joseph Gordon-Levitt, Elliot Page, Tom Hardy", "Dom Cobb có khả năng đánh cắp bí mật từ giấc mơ. Để có cơ hội trở về với các con, anh nhận một nhiệm vụ khác thường: gieo một ý tưởng vào tâm trí người khác, giữa những tầng mơ ngày càng khó phân biệt với thực tại."),
        Movie("oppenheimer", "Oppenheimer", "Oppenheimer", 2023, "Tiểu sử · Chính kịch", 180, "T18", false, R.drawable.poster_oppenheimer, 500f / 750f, "Christopher Nolan", "Cillian Murphy, Emily Blunt, Robert Downey Jr., Matt Damon", "Câu chuyện về nhà vật lý J. Robert Oppenheimer và vai trò của ông trong Dự án Manhattan. Những khám phá thay đổi lịch sử kéo theo các câu hỏi sâu sắc về quyền lực, trách nhiệm và hậu quả của khoa học."),
        Movie("batman", "The Batman", "The Batman", 2022, "Hành động · Tội phạm", 176, "T16", true, R.drawable.poster_batman, 500f / 750f, "Matt Reeves", "Robert Pattinson, Zoë Kravitz, Paul Dano, Jeffrey Wright", "Trong những năm đầu bảo vệ Gotham, Batman lần theo chuỗi mật mã do một kẻ sát nhân để lại. Cùng các đồng minh, anh khám phá mạng lưới tham nhũng và tìm lại ý nghĩa của việc trở thành biểu tượng hy vọng."),
        Movie("spirited", "Vùng đất linh hồn", "Spirited Away", 2001, "Hoạt hình · Phiêu lưu", 125, "P", true, R.drawable.poster_spirited, 500f / 750f, "Hayao Miyazaki", "Rumi Hiiragi, Miyu Irino, Mari Natsuki (lồng tiếng)", "Chihiro lạc vào thế giới của các linh hồn và phải làm việc trong một nhà tắm kỳ lạ để cứu cha mẹ. Với sự giúp đỡ của Haku, cô bé học cách vượt qua nỗi sợ và gìn giữ danh tính của chính mình.")
    )
    fun search(query: String, soon: Boolean): List<Movie> {
        val value = normalize(query.trim())
        return all.filter { it.soon == soon && normalize(it.name + " " + it.original).contains(value) }
    }
    private fun normalize(value: String) = Normalizer.normalize(value.lowercase(Locale.ROOT), Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "").replace("\u0111", "d")
}
