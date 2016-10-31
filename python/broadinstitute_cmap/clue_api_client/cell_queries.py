import logging
import setup_logger

__authors__ = "David L. Lahr"
__email__ = "dlahr@broadinstitute.org"


logger = logging.getLogger(setup_logger.LOGGER_NAME)


def is_cell_line_in_api(clue_api_client, cell_id):
        query_result = clue_api_client.run_count_query("cells", {"cell_id":cell_id})
        logger.debug("query_result:  {}".format(query_result))
        return query_result["count"] == 1