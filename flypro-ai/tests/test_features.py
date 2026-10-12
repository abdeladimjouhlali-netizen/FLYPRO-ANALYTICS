from app.ml.features import FEATURE_COLUMNS, events_to_dataframe
from app.ml.preprocessor import FeaturePreprocessor
from tests.conftest import make_training_events


def test_feature_engineering_columns():
    events = make_training_events(5)
    frame = events_to_dataframe(events)
    assert list(frame.columns) == FEATURE_COLUMNS
    assert len(frame) == 5
    assert frame["hour"].between(0, 23).all()


def test_preprocessor_fit_transform_shape():
    events = make_training_events(25)
    preprocessor = FeaturePreprocessor()
    matrix = preprocessor.fit_transform(events)
    assert matrix.shape == (25, len(FEATURE_COLUMNS))
    assert preprocessor.is_fitted
